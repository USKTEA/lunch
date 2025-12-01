import React, { useEffect } from "react";
import styled from "styled-components";
import { useNavigate } from "react-router-dom";
import useImageStore from "../../hooks/useImageStore";
import useRestaurantStore from "../../hooks/useRestaurantStore";
import useReviewStore from "../../hooks/useReviewStore";
import { userStore } from "../../stores/UserStore";
import StarRatingInput from "../ui/StarRatingInput";
import ImageUploader from "../ui/ImageUploader";

const Overlay = styled.div`
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
`;

const Modal = styled.div`
    background: white;
    border-radius: 12px;
    width: 90%;
    max-width: 500px;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
`;

const Header = styled.div`
    padding: 20px 24px;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    align-items: center;
    justify-content: space-between;
`;

const Title = styled.h2`
    font-size: 20px;
    font-weight: 600;
    margin: 0;
    color: #202124;
`;

const CloseButton = styled.button`
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    color: #666;
    padding: 0;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background 0.2s;

    &:hover {
        background: #f1f3f4;
    }
`;

const Content = styled.div`
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 24px;
`;

const Section = styled.div`
    display: flex;
    flex-direction: column;
    gap: 8px;
`;

const Label = styled.label`
    font-size: 14px;
    font-weight: 600;
    color: #202124;
`;

const Textarea = styled.textarea`
    width: 100%;
    min-height: 120px;
    padding: 12px;
    border: 1px solid #d0d0d0;
    border-radius: 8px;
    font-size: 14px;
    font-family: inherit;
    resize: vertical;
    transition: border-color 0.2s;

    &:focus {
        outline: none;
        border-color: #1a73e8;
    }

    &::placeholder {
        color: #999;
    }
`;

const CharCount = styled.div`
    font-size: 12px;
    color: ${props => props.$over ? "#d93025" : "#5f6368"};
    text-align: right;
`;

const ErrorMessage = styled.div`
    padding: 12px;
    background: #fef7f7;
    border: 1px solid #f5c6cb;
    border-radius: 4px;
    color: #d93025;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
`;

const Footer = styled.div`
    padding: 16px 24px;
    border-top: 1px solid #e0e0e0;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
`;

const Button = styled.button`
    padding: 10px 24px;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    border: none;

    &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }
`;

const CancelButton = styled(Button)`
    background: white;
    color: #5f6368;
    border: 1px solid #d0d0d0;

    &:hover:not(:disabled) {
        background: #f8f9fa;
    }
`;

const SubmitButton = styled(Button)`
    background: #1a73e8;
    color: white;

    &:hover:not(:disabled) {
        background: #1765cc;
    }

    &:active:not(:disabled) {
        background: #1557b0;
    }
`;

const MAX_CONTENT_LENGTH = 100;

/**
 * 리뷰 작성 모달
 */
function ReviewWriteModal({ isOpen, onClose, onSuccess }) {
  const navigate = useNavigate();
  const restaurantStore = useRestaurantStore();
  const reviewStore = useReviewStore();
  const imageStore = useImageStore();

  const selectedRestaurant = restaurantStore.selectedRestaurant;
  const validation = reviewStore.isDraftValid();
  const draftReview = reviewStore.draftReview;
  const error = reviewStore.error;
  const isSubmitting = reviewStore.loading;

  // 로그인 체크 및 초기화
  useEffect(() => {
    if (isOpen) {
      if (!userStore.user) {
        alert("리뷰를 작성하려면 로그인이 필요합니다.");
        onClose();
        navigate("/web/login");
        return;
      }
      // 모달 열 때 초기화
      reviewStore.resetDraftReview();
      imageStore.reset();
    }
  }, [isOpen]);

  const handleSubmit = async () => {
    const imageUrls = await imageStore.uploadAllImages("REVIEW");

    const imageNames = Array.from(imageStore.images.keys());

    const uploaded = await reviewStore.submitReview(
      selectedRestaurant?.restaurantManagementNumber,
      imageNames,
      imageUrls
    );

    if (uploaded) {
      onClose();
      // 리뷰 작성 성공 시 콜백 호출
      if (onSuccess) {
        onSuccess();
      }
    }
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  if (!isOpen) {
    return <></>;
  }

  return (
    <Overlay onClick={handleOverlayClick}>
      <Modal>
        <Header>
          <Title>{selectedRestaurant?.name || "레스토랑"} 리뷰 작성</Title>
          <CloseButton onClick={onClose}>×</CloseButton>
        </Header>

        <Content>
          {/* 별점 */}
          <Section>
            <Label>별점 *</Label>
            <StarRatingInput
              value={draftReview.rating}
              onChange={(rating) => reviewStore.setDraftRating(rating)}
            />
          </Section>

          {/* 리뷰 내용 */}
          <Section>
            <Label>리뷰 *</Label>
            <Textarea
              placeholder="이 식당에 대한 솔직한 리뷰를 남겨주세요."
              value={draftReview.content}
              onChange={(e) => reviewStore.setDraftContent(e.target.value)}
              maxLength={MAX_CONTENT_LENGTH + 10}
            />
            <CharCount $over={validation.isContentOver}>
              {draftReview.content.length} / {MAX_CONTENT_LENGTH}
            </CharCount>
          </Section>

          {/* 이미지 업로드 */}
          <Section>
            <Label>사진 (선택)</Label>
            <ImageUploader />
          </Section>

          {/* 에러 메시지 */}
          {error && (
            <ErrorMessage>
              ⚠ {error}
            </ErrorMessage>
          )}
        </Content>

        <Footer>
          <CancelButton onClick={onClose} disabled={isSubmitting}>
            취소
          </CancelButton>
          <SubmitButton onClick={handleSubmit} disabled={!validation.isValid || isSubmitting}>
            {isSubmitting ? "등록 중..." : "리뷰 등록"}
          </SubmitButton>
        </Footer>
      </Modal>
    </Overlay>
  );
}

export default ReviewWriteModal;
