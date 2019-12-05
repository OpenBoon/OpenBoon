export const onBlur = ({ container, setMenuOpen }) => ({ relatedTarget }) => {
  if (
    !relatedTarget ||
    !container.current ||
    !container.current.contains(relatedTarget)
  ) {
    setMenuOpen(false)
  }
}
