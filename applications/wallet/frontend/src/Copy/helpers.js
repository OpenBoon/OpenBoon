export const onCopy = ({ copyRef }) => {
  copyRef.current.select()
  document.execCommand('copy')
  copyRef.current.blur()
}
