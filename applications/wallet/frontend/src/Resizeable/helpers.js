export const calculateDelta = ({ width, setWidth }) => (e, { deltaX }) => {
  setWidth(Math.max(10, width - deltaX))
}
