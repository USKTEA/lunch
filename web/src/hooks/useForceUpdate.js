import { useState } from 'react';

export default function useForceUpdate() {
  const [, setCount] = useState(0);
  return () => setCount((count) => count + 1);
}
