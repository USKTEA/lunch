import Store from './Store';
import { MOCK_PLACES } from '../data/places.mock';

class PlaceStore extends Store {
  places = [...MOCK_PLACES];

  constructor() {
    super();
  }
}

export const placeStore = new PlaceStore();
