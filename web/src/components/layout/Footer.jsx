import React from 'react';
import styled from 'styled-components';

const FooterContainer = styled.footer`
  height: 50px;
  background: #f7f7f7;
  border-top: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  flex-shrink: 0;
`;

const Copyright = styled.p`
  font-size: 13px;
  color: #999;
`;

function Footer() {
  return (
    <FooterContainer>
      <Copyright>© KST. All rights reserved.</Copyright>
    </FooterContainer>
  );
}

export default Footer;
