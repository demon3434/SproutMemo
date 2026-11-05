import logging
import os
import socket
from typing import Optional
from zeroconf import IPVersion, ServiceInfo
from zeroconf.asyncio import AsyncZeroconf
from app.core.config import settings

logger = logging.getLogger("SproutMemo.mdns_discovery")


class ServiceDiscoveryBroadcaster:
    """
    Standard mDNS (DNS-SD) service broadcaster for SproutMemo server.
    Uses AsyncZeroconf to integrate seamlessly with FastAPI asyncio event loop.
    """

    def __init__(self):
        self.aiozc: Optional[AsyncZeroconf] = None
        self.info: Optional[ServiceInfo] = None
        self.active_host: Optional[str] = None
        self.active_port: Optional[int] = None

    async def start(self, host: str, port: int) -> bool:
        await self.stop()

        if not host or not host.strip():
            logger.warning("mDNS: No host IP provided. Skipping broadcast.")
            return False

        self.active_host = host.strip()
        self.active_port = int(port)

        try:
            self.aiozc = AsyncZeroconf(ip_version=IPVersion.V4Only)

            try:
                ip_bytes = socket.inet_aton(self.active_host)
            except OSError:
                logger.error(f"mDNS: Invalid IP address format: {self.active_host}")
                return False

            service_type = settings.SERVICE_DISCOVERY_TYPE.strip()
            if not service_type.endswith("."):
                service_type = f"{service_type}."
            if not service_type.endswith(".local."):
                if service_type.endswith(".local"):
                    service_type = f"{service_type}."
                else:
                    service_type = f"{service_type.rstrip('.')}local."

            instance_name = f"{settings.SERVICE_DISCOVERY_NAME}.{service_type}"

            properties = {
                b"host": self.active_host.encode("utf-8"),
                b"mappedPort": str(self.active_port).encode("utf-8"),
                b"url": f"http://{self.active_host}:{self.active_port}".encode("utf-8"),
                b"version": b"1.0",
                b"path": b"/api/v1",
            }

            self.info = ServiceInfo(
                type_=service_type,
                name=instance_name,
                addresses=[ip_bytes],
                port=self.active_port,
                properties=properties,
                server=f"{settings.SERVICE_DISCOVERY_NAME.lower()}-server.local.",
            )

            logger.info(
                f"mDNS: Registering service {instance_name} at {self.active_host}:{self.active_port}"
            )
            await self.aiozc.async_register_service(self.info, allow_name_change=True)
            logger.info("mDNS: Service registration complete.")
            return True

        except Exception as e:
            logger.exception(f"mDNS: Failed to start service discovery: {e}")
            await self.stop()
            return False

    async def stop(self):
        if self.aiozc:
            try:
                if self.info:
                    logger.info("mDNS: Unregistering service discovery...")
                    await self.aiozc.async_unregister_service(self.info)
            except Exception as e:
                logger.error(f"mDNS: Error unregistering service: {e}")
            finally:
                try:
                    await self.aiozc.async_close()
                except Exception as e:
                    logger.error(f"mDNS: Error closing AsyncZeroconf: {e}")
                self.aiozc = None
                self.info = None
                self.active_host = None
                self.active_port = None
                logger.info("mDNS: Service discovery stopped.")


broadcaster = ServiceDiscoveryBroadcaster()


def _detect_local_ip() -> str:
    """
    Attempts to detect the machine's primary local LAN IP address,
    filtering out loopback (127.x) and container bridge (172.x) subnets.
    """
    try:
        udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        udp.connect(("8.8.8.8", 80))
        detected = udp.getsockname()[0]
        udp.close()
        if detected and not detected.startswith("127.") and not detected.startswith("172."):
            return detected
    except Exception:
        pass

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("10.255.255.255", 1))
        detected_ip = s.getsockname()[0]
        s.close()
        if detected_ip and not detected_ip.startswith("127.") and not detected_ip.startswith("172."):
            return detected_ip
    except Exception:
        pass

    try:
        ip = socket.gethostbyname(socket.gethostname())
        if ip and not ip.startswith("127.") and not ip.startswith("172."):
            return ip
    except Exception:
        pass

    return ""


async def get_mdns_settings_and_start():
    """
    Reads mDNS settings from config/environment or auto-detected local IP on startup and starts mDNS broadcast.
    """
    try:
        host = ""
        if settings.SERVICE_ADVERTISE_HOST and settings.SERVICE_ADVERTISE_HOST.strip():
            host = settings.SERVICE_ADVERTISE_HOST.strip()
        elif settings.HOST_IP and settings.HOST_IP.strip():
            host = settings.HOST_IP.strip()
        else:
            host = _detect_local_ip()

        port = settings.SERVICE_ADVERTISE_PORT or settings.HOST_PORT or 8080

        if host:
            logger.info(f"Startup: Starting mDNS broadcast for {host}:{port}")
            await broadcaster.start(host, port)
        else:
            logger.info("Startup: No host IP configured or detected. Zero-configuration service discovery is idle.")
    except Exception as e:
        logger.error(f"Startup: Failed to initialize mDNS: {e}", exc_info=True)


async def stop_mdns_discovery():
    """
    Stops mDNS broadcast and unregisters service.
    """
    try:
        await broadcaster.stop()
    except Exception as e:
        logger.error(f"Shutdown: Failed to stop mDNS: {e}", exc_info=True)
