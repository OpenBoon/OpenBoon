import PropTypes from 'prop-types'

import PenSvg from '../Icons/pen.svg'
import CrossSvg from '../Icons/cross.svg'

import { colors, constants, spacing, typography } from '../Styles'

import { ACTIONS } from '../DataVisualization/reducer'

import Button, { VARIANTS } from '../Button'

const ICON_PADDING = 6

const ChartHeader = ({ attribute, chartIndex, dispatch, setIsEditing }) => {
  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        marginRight: -spacing.base,
        ':hover': {
          button: {
            svg: { opacity: 1 },
          },
        },
      }}
    >
      <h3
        css={{
          flex: 1,
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingRight: spacing.base,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
        }}
      >
        {attribute}
      </h3>

      <Button
        title="Edit"
        aria-label="Edit Chart"
        variant={VARIANTS.ICON}
        css={{
          padding: ICON_PADDING,
          svg: {
            opacity: 0,
          },
          ':hover, &.focus-visible:focus': {
            backgroundColor: colors.structure.smoke,
          },
        }}
        onClick={() => {
          setIsEditing(true)
        }}
      >
        <PenSvg height={constants.iconSize} />
      </Button>

      <div css={{ width: spacing.mini }} />

      <Button
        title="Delete"
        aria-label="Delete Chart"
        variant={VARIANTS.ICON}
        css={{
          padding: ICON_PADDING,
          svg: {
            opacity: 0,
          },
          ':hover, &.focus-visible:focus': {
            backgroundColor: colors.structure.smoke,
          },
        }}
        onClick={() =>
          dispatch({ type: ACTIONS.DELETE, payload: { chartIndex } })
        }
      >
        <CrossSvg height={constants.iconSize} />
      </Button>
    </div>
  )
}

ChartHeader.propTypes = {
  attribute: PropTypes.string.isRequired,
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
  setIsEditing: PropTypes.func.isRequired,
}

export default ChartHeader
