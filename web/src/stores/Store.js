export default class Store {
  constructor() {
    this.listeners = new Set();
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  unsubscribe(listener) {
    this.listeners.delete(listener);
  }

  publish() {
    this.listeners.forEach((listener) => listener());
  }
}
