import PropTypes from 'prop-types'

import PenSvg from '../Icons/pen.svg'
import CrossSvg from '../Icons/cross.svg'

import { colors, spacing, typography } from '../Styles'

import { ACTIONS } from '../DataVisualization/reducer'

import Button, { VARIANTS } from '../Button'

const ICON_PADDING = 6

const ChartsHeader = ({ attribute, chartIndex, dispatch }) => {
  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        paddingTop: spacing.mini,
        ':hover': {
          button: {
            color: colors.structure.white,
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
        }}
      >
        {attribute}
      </h3>
      <Button
        title="Edit"
        aria-label="Edit Chart"
        variant={VARIANTS.NEUTRAL}
        css={{
          padding: ICON_PADDING,
          svg: {
            color: colors.structure.steel,
          },
          ':focus, :hover, &.focus-visible:focus': {
            svg: { opacity: 1, color: colors.structure.white },
            backgroundColor: colors.structure.smoke,
          },
        }}
      >
        <PenSvg width={20} css={{ opacity: 0 }} />
      </Button>
      <div css={{ width: spacing.mini }} />
      <Button
        title="Delete"
        aria-label="Delete Chart"
        variant={VARIANTS.NEUTRAL}
        css={{
          padding: ICON_PADDING,
          svg: {
            color: colors.structure.steel,
          },
          ':focus, :hover, &.focus-visible:focus': {
            svg: { opacity: 1, color: colors.structure.white },
            backgroundColor: colors.structure.smoke,
          },
        }}
        onClick={() =>
          dispatch({ type: ACTIONS.DELETE, payload: { chartIndex } })
        }
      >
        <CrossSvg width={20} css={{ opacity: 0 }} />
      </Button>
    </div>
  )
}

ChartsHeader.propTypes = {
  attribute: PropTypes.string.isRequired,
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartsHeader
