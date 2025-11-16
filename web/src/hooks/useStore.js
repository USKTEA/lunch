import { useEffect } from 'react';
import useForceUpdate from './useForceUpdate';

export default function useStore(store) {
  const forceUpdate = useForceUpdate();

  useEffect(() => {
    return store.subscribe(forceUpdate);
  }, [store, forceUpdate]);

  return store;
}
