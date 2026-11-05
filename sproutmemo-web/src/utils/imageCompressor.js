/**
 * Image compressor utility function.
 * Resizes the image so that its longest side is at most 1920px,
 * and steps down JPEG/PNG quality until the file size is under 1MB or quality is 0.6.
 */
export function compressImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (e) => {
      const img = new Image();
      img.src = e.target.result;
      img.onload = async () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;
        
        const maxEdge = 1920;
        if (width > maxEdge || height > maxEdge) {
          if (width > height) {
            height = Math.round((height * maxEdge) / width);
            width = maxEdge;
          } else {
            width = Math.round((width * maxEdge) / height);
            height = maxEdge;
          }
        }
        
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        
        const mimeType = file.type || 'image/jpeg';
        let quality = 0.9;
        
        const canvasToBlob = (q) => {
          return new Promise((resBlob) => {
            canvas.toBlob((blob) => resBlob(blob), mimeType, q);
          });
        };

        try {
          let blob = await canvasToBlob(quality);
          while (blob && blob.size > 1024 * 1024 && quality > 0.6) {
            quality -= 0.05;
            blob = await canvasToBlob(quality);
          }
          
          if (blob) {
            resolve(new File([blob], file.name, { type: mimeType }));
          } else {
            resolve(file);
          }
        } catch (err) {
          reject(err);
        }
      };
      img.onerror = reject;
    };
    reader.onerror = reject;
  });
}

/**
 * Physical image rotation utility using HTML5 Canvas.
 * Rotates a File object or image URL by the specified angle (e.g. ±90 deg),
 * enforces max edge <= 1920px, and steps down JPEG quality until <= 1MB.
 * Returns a new File object with the rotated image data.
 */
export function rotateImageFile(fileOrUrl, angle) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.src = typeof fileOrUrl === 'string' ? fileOrUrl : URL.createObjectURL(fileOrUrl);
    
    img.onload = async () => {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      
      let drawWidth = img.width;
      let drawHeight = img.height;
      const maxEdge = 1920;
      if (drawWidth > maxEdge || drawHeight > maxEdge) {
        if (drawWidth > drawHeight) {
          drawHeight = Math.round((drawHeight * maxEdge) / drawWidth);
          drawWidth = maxEdge;
        } else {
          drawWidth = Math.round((drawWidth * maxEdge) / drawHeight);
          drawHeight = maxEdge;
        }
      }
      
      const normalizedAngle = ((angle % 360) + 360) % 360;
      const rad = (normalizedAngle * Math.PI) / 180;
      const is90 = normalizedAngle === 90 || normalizedAngle === 270;
      
      canvas.width = is90 ? drawHeight : drawWidth;
      canvas.height = is90 ? drawWidth : drawHeight;
      
      ctx.translate(canvas.width / 2, canvas.height / 2);
      ctx.rotate(rad);
      ctx.drawImage(img, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight);
      
      const mimeType = 'image/jpeg';
      let quality = 0.9;
      
      const canvasToBlob = (q) => {
        return new Promise((resBlob) => {
          canvas.toBlob((blob) => resBlob(blob), mimeType, q);
        });
      };

      try {
        let blob = await canvasToBlob(quality);
        while (blob && blob.size > 1024 * 1024 && quality > 0.6) {
          quality -= 0.05;
          blob = await canvasToBlob(quality);
        }
        
        if (typeof fileOrUrl !== 'string') {
          URL.revokeObjectURL(img.src);
        }
        
        if (!blob) {
          reject(new Error('Canvas to Blob failed'));
          return;
        }
        
        const fileName = (fileOrUrl instanceof File) ? fileOrUrl.name : 'rotated.jpg';
        resolve(new File([blob], fileName, { type: mimeType }));
      } catch (err) {
        if (typeof fileOrUrl !== 'string') {
          URL.revokeObjectURL(img.src);
        }
        reject(err);
      }
    };
    
    img.onerror = (err) => {
      if (typeof fileOrUrl !== 'string') {
        URL.revokeObjectURL(img.src);
      }
      reject(err);
    };
  });
}
