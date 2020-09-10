export const handleMouseUp = ({ minWidth, setOpenPanel }) => ({ size }) => {
  if (size < minWidth) setOpenPanel({ value: '' })
}
