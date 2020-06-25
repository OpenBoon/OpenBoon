import PropTypes from 'prop-types'

import EditSvg from '../Icons/edit.svg'
import CrossSvg from '../Icons/cross.svg'

import { colors, spacing, typography } from '../Styles'

import { ACTIONS } from '../DataVisualization/reducer'

import Button, { VARIANTS } from '../Button'

const ChartsHeader = ({ attribute, chartIndex, dispatch }) => {
  const splitAttribute = attribute.split('.')
  const title = splitAttribute[splitAttribute.length - 1]

  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
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
        {title}
      </h3>
      <Button
        title="Edit Chart"
        variant={VARIANTS.CARD_ICON}
        css={{
          ':focus': {
            svg: { opacity: 1, color: colors.structure.white },
          },
          ':hover': {
            svg: { opacity: 1, color: colors.structure.white },
          },
        }}
      >
        <EditSvg
          width={20}
          color={colors.structure.steel}
          css={{ opacity: 0 }}
        />
      </Button>
      <div css={{ width: spacing.mini }} />
      <Button
        title="Delete Chart"
        variant={VARIANTS.CARD_ICON}
        css={{
          ':focus': {
            svg: { opacity: 1, color: colors.structure.white },
          },
          ':hover': {
            svg: { opacity: 1, color: colors.structure.white },
          },
        }}
        onClick={() =>
          dispatch({ type: ACTIONS.DELETE, payload: { chartIndex } })
        }
      >
        <CrossSvg
          width={20}
          color={colors.structure.steel}
          css={{ opacity: 0 }}
        />
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
