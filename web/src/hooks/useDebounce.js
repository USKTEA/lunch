import { useEffect, useState } from 'react';

/**
 * 디바운스 Hook
 * @param {any} value - 디바운스할 값
 * @param {number} delay - 딜레이 시간 (ms)
 * @returns {any} 디바운스된 값
 */
export default function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return debouncedValue;
}