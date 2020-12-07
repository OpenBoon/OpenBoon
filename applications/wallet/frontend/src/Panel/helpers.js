export const onMouseUp = ({ minWidth }) => ({ newSize }) => {
  if (newSize < minWidth) {
    return {
      openPanel: '',
    }
  }
  return {}
}
