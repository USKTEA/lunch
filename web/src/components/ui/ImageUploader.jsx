import React, { useRef } from 'react';
import styled from 'styled-components';
import useImageStore from '../../hooks/useImageStore';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const ImageGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
`;

const ImagePreview = styled.div`
  position: relative;
  width: 100%;
  padding-top: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
`;

const PreviewImage = styled.img`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
`;

const RemoveButton = styled.button`
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 0, 0, 0.8);
  }
`;


const AddButton = styled.button`
  width: 100%;
  height: 100px;
  border: 2px dashed #d0d0d0;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
  transition: all 0.2s;

  &:hover {
    border-color: #1a73e8;
    background: #f0f7ff;
    color: #1a73e8;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }
`;

const HiddenInput = styled.input`
  display: none;
`;

const ErrorText = styled.div`
  color: #d93025;
  font-size: 12px;
  margin-top: 4px;
`;

/**
 * 이미지 업로더 컴포넌트
 */
function ImageUploader() {
  const imageStore = useImageStore();
  const fileInputRef = useRef(null);

  const images = Array.from(imageStore.images.entries()).map(([id, data]) => ({
    id,
    ...data
  }));
  const maxImages = imageStore.MAX_IMAGES;
  const allowedTypes = imageStore.ALLOWED_TYPES;
  const error = imageStore.error;

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files || []);

    files.forEach(file => {
      imageStore.addImagePreview(file);
    });

    // input 초기화
    e.target.value = '';
  };

  const handleRemove = (imageId) => {
    imageStore.removeImage(imageId);
    imageStore.clearError();
  };

  const handleAddClick = () => {
    fileInputRef.current?.click();
  };

  return (
    <Container>
      {images.length > 0 && (
        <ImageGrid>
          {images.map(image => (
            <ImagePreview key={image.id}>
              <PreviewImage src={image.previewUrl} alt="미리보기" />
              <RemoveButton onClick={() => handleRemove(image.id)}>
                ×
              </RemoveButton>
            </ImagePreview>
          ))}
        </ImageGrid>
      )}

      {images.length < maxImages && (
        <>
          <AddButton
            type="button"
            onClick={handleAddClick}
          >
            <span style={{ fontSize: '24px' }}>📷</span>
            <span>사진 추가 ({images.length}/{maxImages})</span>
          </AddButton>
          <HiddenInput
            ref={fileInputRef}
            type="file"
            accept={allowedTypes.join(',')}
            multiple
            onChange={handleFileSelect}
          />
        </>
      )}

      {error && (
        <ErrorText>{error}</ErrorText>
      )}
    </Container>
  );
}

export default ImageUploader;
