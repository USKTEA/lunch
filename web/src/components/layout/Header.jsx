import React from "react";
import styled from "styled-components";
import useUserStore from "../../hooks/useUserStore";

const HeaderContainer = styled.header`
    height: 60px;
    background: #ffffff;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    flex-shrink: 0;
`;

const Title = styled.h1`
    font-size: 20px;
    font-weight: 700;
    color: #333;
`;

const UserInfo = styled.div`
    display: flex;
    align-items: center;
    gap: 12px;
`;

const UserName = styled.span`
    font-size: 14px;
    font-weight: 500;
    color: #666;
`;

function Header() {
  const userStore = useUserStore();
  const user = userStore.user

  if (!user) {
    return (
      <HeaderContainer>
        <Title>🍱 HT beyond Lunch</Title>
        <UserInfo>
          <UserName>loading...</UserName>
        </UserInfo>
      </HeaderContainer>
    );
  }

  return (
    <HeaderContainer>
      <Title>🍱 HT beyond Lunch</Title>
      <UserInfo>
        <UserName>{userStore.user?.nickname ?? ""}</UserName>
      </UserInfo>
    </HeaderContainer>
  );
}

export default Header;
