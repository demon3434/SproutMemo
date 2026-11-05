def test_ping(client):
    response = client.get("/api/v1/ping")
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["code"] == 200
    assert json_data["msg"] == "success"
    assert json_data["data"] == "pong"
