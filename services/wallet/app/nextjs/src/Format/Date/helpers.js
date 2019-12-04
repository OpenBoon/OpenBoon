export const formatFullDate = ({ timestamp }) => {
  const formatOptions = {
    month: '2-digit',
    day: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    hour12: false,
    minute: '2-digit',
    second: '2-digit',
  }
  return Intl.DateTimeFormat('en-US', formatOptions)
    .format(new Date(timestamp))
    .replace(/\//g, '-')
    .replace(',', '')
}
