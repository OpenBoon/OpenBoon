const GRADIENT = [
  [100, [0, 39, 107]],
  [63, [0, 149, 255]],
  [0, [255, 255, 255]],
]

const getClosestColors = ({ percent }) => {
  const index = GRADIENT.findIndex((c) => c[0] < percent)

  const leftBound = GRADIENT[index - 1] || GRADIENT[GRADIENT.length - 2]
  const rightBound = GRADIENT[index] || GRADIENT[GRADIENT.length - 1]

  const ratio = (percent - leftBound[0]) / (rightBound[0] - leftBound[0])

  return { leftColor: leftBound[1], rightColor: rightBound[1], ratio }
}

// http://jsfiddle.net/pwvz1gk9/
export const getColor = ({ percent: p }) => {
  const percent = Math.max(0, Math.min(p, 100))

  const { leftColor, rightColor, ratio } = getClosestColors({ percent })

  const i = ratio * 2 - 1
  const j = (i / 1 + 1) / 2
  const k = 1 - j

  const rgb = [
    Math.round(leftColor[0] * k + rightColor[0] * j),
    Math.round(leftColor[1] * k + rightColor[1] * j),
    Math.round(leftColor[2] * k + rightColor[2] * j),
  ]

  return `rgb(${rgb})`
}
