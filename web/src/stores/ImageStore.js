import Store from './Store';
import { apiService } from '../service/ApiService';

/**
 * 이미지 업로드 상태 관리
 * S3 Presigned URL 방식으로 이미지 업로드
 */
class ImageStore extends Store {
  // 검증 상수
  MAX_IMAGES = 5;
  MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  images = new Map(); // key: UUID, value: { file, previewUrl }
  error = null;

  constructor() {
    super();
  }

  /**
   * 파일 검증
   * @returns {{ isValid: boolean, errorMessage: string | null }}
   */
  validateFile(file) {
    // 파일 타입 검증
    if (!this.ALLOWED_TYPES.includes(file.type)) {
      return {
        isValid: false,
        errorMessage: 'JPG, PNG, WEBP 형식의 이미지만 업로드 가능합니다.'
      };
    }

    // 파일 크기 검증
    if (file.size > this.MAX_FILE_SIZE) {
      return {
        isValid: false,
        errorMessage: '10MB 이하의 이미지만 업로드 가능합니다.'
      };
    }

    // 최대 개수 검증
    if (this.images.size >= this.MAX_IMAGES) {
      return {
        isValid: false,
        errorMessage: `최대 ${this.MAX_IMAGES}개까지 업로드 가능합니다.`
      };
    }

    return { isValid: true, errorMessage: null };
  }

  /**
   * 이미지 선택 시 로컬 미리보기 추가
   * 검증 후 추가
   */
  addImagePreview(file) {
    const validation = this.validateFile(file);
    if (!validation.isValid) {
      this.error = validation.errorMessage;
      this.publish();
      return null;
    }

    const id = crypto.randomUUID();
    const previewUrl = URL.createObjectURL(file);

    this.images.set(id, {
      file,
      previewUrl
    });

    this.error = null;
    this.publish();
    return id;
  }

  /**
   * 이미지 제거
   */
  removeImage(id) {
    const image = this.images.get(id);
    if (image?.previewUrl) {
      URL.revokeObjectURL(image.previewUrl);
    }

    this.images.delete(id);
    this.publish();
  }

  /**
   * 모든 이미지 업로드
   * 1. 모든 이미지 메타정보를 한 번에 서버로 전달하여 presigned URL 리스트 받기
   * 2. 받은 presigned URL로 각각 S3에 병렬 업로드
   * 3. 업로드된 URL 리스트 반환
   *
   * @param {string} context - 이미지 업로드 컨텍스트 ('REVIEW' 등)
   */
  async uploadAllImages(context) {
    if (this.images.size === 0) {
      return [];
    }

    try {
      // 1. 백엔드로 presigned URL 요청
      const imageMetas = Array.from(this.images.entries()).map(([id, imageData]) => ({
        name: id,
        imageSize: imageData.file.size,
        contentType: imageData.file.type
      }));

      const response = await apiService.createPresignedUrls({
        context,
        imageMetas
      });

      // response 형태: { preSignedUrls: [{ name: UUID, url: URL }, ...] }
      const presignedUrlMap = response.preSignedUrls.reduce((acc, item) => {
        acc[item.name] = item.url;
        return acc;
      }, {});

      // 2. S3에 병렬 업로드
      const uploadPromises = Array.from(this.images.entries()).map(([id, imageData]) => {
        const presignedUrl = presignedUrlMap[id];
        console.log('Uploading to S3:', {
          id,
          fileName: imageData.file.name,
          fileSize: imageData.file.size,
          fileType: imageData.file.type,
          presignedUrl: presignedUrl.substring(0, 100) + '...'
        });
        return apiService.uploadToS3(presignedUrl, imageData.file)
          .then(() => presignedUrl.split('?')[0]) // 쿼리 파라미터 제거한 URL 반환
          .catch(error => {
            console.error('Upload failed for:', id, error.response?.data || error.message);
            throw error;
          });
      });

      const results = await Promise.allSettled(uploadPromises);

      console.log('Upload results:', results);
      const successResults = results
        .filter(r => r.status === 'fulfilled')
        .map(r => r.value);

      const failedCount = results.filter(r => r.status === 'rejected').length;
      if (failedCount > 0) {
        this.error = `${failedCount}개 이미지 업로드 실패`;
        throw new Error(`${failedCount}개 이미지 업로드 실패`);
      }

      return successResults;
    } catch (error) {
      this.error = error.message;
      this.publish();
      throw error;
    }
  }

  /**
   * 초기화
   */
  reset() {
    // 미리보기 URL 메모리 해제
    this.images.forEach(image => {
      if (image.previewUrl) {
        URL.revokeObjectURL(image.previewUrl);
      }
    });

    this.images.clear();
    this.error = null;
    this.publish();
  }

  clearError() {
    this.error = null;
    this.publish();
  }
}

export const imageStore = new ImageStore();
