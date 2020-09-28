export const getMetadata = (event) => {
  const metadata = Object.values(event.target.activeCues).reduce(
    (acc, { text }) => {
      const { timeline, track, score } = JSON.parse(text)
      const timelineCues = acc[timeline] || []
      return { ...acc, [timeline]: [...timelineCues, { label: track, score }] }
    },
    {},
  )

  return metadata
}
