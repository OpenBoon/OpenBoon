export const getMetadata = (event) => {
  const {
    target: { label, activeCues },
  } = event

  if (!activeCues || activeCues.length === 0) return { [label]: [] }

  const metadata = Object.values(event.target.activeCues).reduce(
    (acc, { text }) => {
      const { timeline, content, score } = JSON.parse(text)

      const timelineCues = acc[timeline] || []

      return {
        ...acc,
        [timeline]: [...timelineCues, { content: content.join(', '), score }],
      }
    },
    {},
  )

  return metadata
}
