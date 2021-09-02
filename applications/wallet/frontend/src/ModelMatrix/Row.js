import { useEffect } from 'react'
import PropTypes from 'prop-types'
import Tippy from '@tippyjs/react/headless'

import { colors, constants, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import { useScroller } from '../Scroll/helpers'
import { ACTIONS, reducer as resizeableReducer } from '../Resizeable/reducer'

import { getColor, PANEL_WIDTH } from './helpers'

import ModelMatrixTooltip from './Tooltip'

const CONTRAST_THRESHOLD = 69

const ModelMatrixRow = ({ matrix, settings, label, index, dispatch }) => {
  const rowRef = useScroller({
    namespace: 'ModelMatrixHorizontal',
    isWheelEmitter: true,
    isWheelListener: true,
    isScrollEmitter: true,
  })

  const [{ isOpen }, setPreviewSettings] = useLocalStorage({
    key: `Resizeable.ModelMatrixPreview`,
    reducer: resizeableReducer,
    initialState: {
      size: PANEL_WIDTH,
      originSize: 0,
      isOpen: false,
    },
  })

  /* istanbul ignore next */
  useEffect(() => {
    setTimeout(() => {
      rowRef.current.dispatchEvent(new Event('scroll'))
    }, 0)
  }, [settings.zoom, rowRef])

  const rowTotal = matrix.matrix[index].reduce(
    (previous, current) => previous + current,
    0,
  )

  const cellDimension = (settings.height / matrix.labels.length) * settings.zoom

  return (
    <div
      key={label}
      css={{
        display: 'flex',
        height: cellDimension,
        filter: `grayscale(${matrix.unappliedChanges ? 1 : 0})`,
      }}
    >
      <div
        css={{
          width: settings.labelsWidth,
          paddingLeft: spacing.normal,
          paddingRight: spacing.normal,
          display: 'flex',
          alignItems: 'center',
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.bold,
          borderRight: constants.borders.regular.coal,
          borderBottom:
            index === matrix.labels.length - 1
              ? constants.borders.regular.transparent
              : constants.borders.regular.coal,
        }}
      >
        <span
          title={`${label} (${rowTotal})`}
          css={{
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {label}{' '}
        </span>
        <span
          css={{
            color: colors.structure.zinc,
            paddingLeft: spacing.small,
            fontWeight: typography.weight.regular,
          }}
        >
          ({rowTotal})
        </span>
      </div>

      <div
        ref={rowRef}
        css={{
          flex: 1,
          display: 'flex',
          overflow: 'hidden',
        }}
      >
        {matrix.matrix[index].map((value, col) => {
          const percent = rowTotal === 0 ? 0 : (value / rowTotal) * 100
          const isSelected =
            settings.selectedCell[0] === index &&
            settings.selectedCell[1] === col

          return (
            <Tippy
              key={matrix.labels[col]}
              render={() => (
                <ModelMatrixTooltip
                  matrix={matrix}
                  index={index}
                  value={value}
                  rowTotal={rowTotal}
                  percent={percent}
                  col={col}
                />
              )}
            >
              <div css={{ display: 'inline' }}>
                <button
                  type="button"
                  aria-label={`${matrix.labels[index]} / ${
                    matrix.labels[col]
                  }: ${value}${settings.isNormalized ? '%' : ''}`}
                  css={{
                    width: cellDimension,
                    height: cellDimension,
                    padding: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    backgroundColor: getColor({ percent }),
                    border: isSelected
                      ? constants.borders.keyOneLarge
                      : constants.borders.large.transparent,
                    color:
                      percent > CONTRAST_THRESHOLD
                        ? colors.structure.white
                        : colors.structure.coal,
                    ':hover': {
                      border: constants.borders.keyTwoLarge,
                    },
                  }}
                  onClick={() => {
                    dispatch({
                      selectedCell: isSelected ? [] : [index, col],
                    })

                    if (!isOpen && !isSelected) {
                      setPreviewSettings({
                        type: ACTIONS.OPEN,
                        payload: {
                          minSize: PANEL_WIDTH,
                        },
                      })
                    }

                    if (isOpen && isSelected) {
                      setPreviewSettings({
                        type: ACTIONS.CLOSE,
                      })
                    }
                  }}
                >
                  <div
                    css={{
                      margin: 'auto',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {settings.isNormalized ? `${Math.round(percent)}%` : value}
                  </div>
                </button>
              </div>
            </Tippy>
          )
        })}
      </div>
    </div>
  )
}

ModelMatrixRow.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
    unappliedChanges: PropTypes.bool.isRequired,
  }).isRequired,
  settings: PropTypes.shape({
    height: PropTypes.number.isRequired,
    labelsWidth: PropTypes.number.isRequired,
    zoom: PropTypes.number.isRequired,
    selectedCell: PropTypes.arrayOf(PropTypes.number.isRequired),
    isNormalized: PropTypes.bool.isRequired,
  }).isRequired,
  label: PropTypes.string.isRequired,
  index: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixRow
