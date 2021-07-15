import { useEffect, useRef } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { getScroller } from '../Scroll/helpers'

import { getColor } from './helpers'

const ModelMatrixMinimap = ({
  matrix,
  settings,
  isInteractive,
  isOutOfDate,
}) => {
  const verticalScroller = getScroller({ namespace: 'ModelMatrixVertical' })
  const horizontalScroller = getScroller({ namespace: 'ModelMatrixHorizontal' })

  const minimapRef = useRef()

  const width = Math.min(
    (((settings.width - settings.labelsWidth) / settings.height) * 100) /
      settings.zoom,
    100,
  )

  const height = Math.min(100 / settings.zoom, 100)

  useEffect(() => {
    const horizontalDeregister = horizontalScroller.register({
      eventName: 'scroll',
      callback: /* istanbul ignore next */ ({ node }) => {
        if (!minimapRef.current) return

        const left = (node.scrollLeft / node.clientWidth) * width

        minimapRef.current.style.left = `${left}%`
      },
    })

    const verticalDeregister = verticalScroller.register({
      eventName: 'scroll',
      callback: /* istanbul ignore next */ ({ node }) => {
        if (!minimapRef.current) return

        const top = (node.scrollTop / node.clientHeight) * height

        minimapRef.current.style.top = `${top}%`
      },
    })

    return () => {
      horizontalDeregister()
      verticalDeregister()
    }
  }, [horizontalScroller, verticalScroller, width, height])

  return (
    <div
      css={{
        opacity: settings.isMinimapOpen ? 1 : 0,
        position: 'relative',
        border: isInteractive
          ? constants.borders.medium.steel
          : constants.borders.large.steel,
        padding: isInteractive ? 0 : spacing.hairline,
        borderRadius: constants.borderRadius.small,
        marginBottom: spacing.base,
        display: 'grid',
        gridTemplate: `repeat(${matrix.labels.length}, 1fr) / repeat(${matrix.labels.length}, 1fr)`,
      }}
    >
      {matrix.labels.map((label, index) => {
        const rowTotal = matrix.matrix[index].reduce(
          (previous, current) => previous + current,
          0,
        )

        return matrix.matrix[index].map((value, col) => {
          const percent = rowTotal === 0 ? 0 : (value / rowTotal) * 100

          return (
            <div
              key={matrix.labels[col]}
              css={{
                filter: `grayscale(${isOutOfDate ? 1 : 0})`,
                paddingBottom: '100%',
                backgroundColor: getColor({ percent }),
                borderRight:
                  isInteractive || col + 1 === matrix.matrix[index].length
                    ? 'none'
                    : constants.borders.regular.coal,
                borderBottom:
                  isInteractive || index + 1 === matrix.labels.length
                    ? 'none'
                    : constants.borders.regular.coal,
              }}
            />
          )
        })
      })}

      {isInteractive && (
        <div
          ref={minimapRef}
          css={{
            position: 'absolute',
            borderStyle: 'solid',
            borderWidth: constants.borderWidths.medium,
            borderColor: colors.signal.warning.base,
            top: 0,
            left: 0,
            width: `${width}%`,
            height: `${height}%`,
          }}
        />
      )}
    </div>
  )
}

ModelMatrixMinimap.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
  }).isRequired,
  settings: PropTypes.shape({
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    labelsWidth: PropTypes.number.isRequired,
    zoom: PropTypes.number.isRequired,
    isMinimapOpen: PropTypes.bool.isRequired,
  }).isRequired,
  isInteractive: PropTypes.bool.isRequired,
  isOutOfDate: PropTypes.bool.isRequired,
}

export default ModelMatrixMinimap
