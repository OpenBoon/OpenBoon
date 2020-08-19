export const formatTitle = ({ buckets, index }) => {
  const { docCount } = buckets[index]

  const interval = buckets[1].key - buckets[0].key

  const lowerScore = buckets[index].key

  const upperScore =
    (buckets[index + 1] && buckets[index + 1].key) || lowerScore + interval

  return `${docCount} (${lowerScore.toFixed(2)} - ${upperScore.toFixed(2)})`
}
