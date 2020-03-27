export const calculateDelta = ({ width, setWidth }) => (e, { deltaX }) => {
  setWidth({ value: Math.max(10, width - deltaX) })
}
