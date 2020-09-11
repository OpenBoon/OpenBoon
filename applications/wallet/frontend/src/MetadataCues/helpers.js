export const getMetadata = (event) => {
  const metadata = Object.values(event.target.activeCues).reduce(
    (acc, { text }) => {
      const { module, label, score } = JSON.parse(text)
      const moduleCues = acc[module] || []
      return { ...acc, [module]: [...moduleCues, { label, score }] }
    },
    {},
  )

  return metadata
}
