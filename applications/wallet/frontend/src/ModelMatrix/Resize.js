import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CirclePlusSvg from '../Icons/circlePlus.svg'
import CircleMinusSvg from '../Icons/circleMinus.svg'

const ModelMatrixResize = ({ zoom, dispatch }) => (
  <div
    css={{
      position: 'absolute',
      bottom: spacing.normal,
      right: spacing.normal,
      display: 'flex',
      border: constants.borders.regular.iron,
      borderRadius: constants.borderRadius.small,
      backgroundColor: colors.structure.lead,
      boxShadow: constants.boxShadows.default,
      zIndex: zIndex.layout.interactive,
      opacity: constants.opacity.eighth,
      paddingTop: spacing.small,
      paddingBottom: spacing.small,
      paddingLeft: spacing.base,
      paddingRight: spacing.base,
    }}
  >
    <Button
      aria-label="Zoom Out"
      onClick={() => {
        if (zoom === 1) return
        dispatch({ zoom: zoom - 1 })
      }}
      isDisabled={zoom === 1}
      variant={VARIANTS.NEUTRAL}
      css={{
        padding: spacing.base,
        ':hover': {
          color: colors.key.one,
        },
        '&[aria-disabled=true]': {
          color: colors.structure.steel,
        },
        opacity: constants.opacity.full,
      }}
    >
      <CircleMinusSvg height={constants.icons.regular} />
    </Button>

    <Button
      aria-label="Zoom In"
      onClick={() => {
        dispatch({ zoom: zoom + 1 })
      }}
      isDisabled={false}
      variant={VARIANTS.NEUTRAL}
      css={{
        padding: spacing.base,
        ':hover': {
          color: colors.key.one,
        },
        '&[aria-disabled=true]': {
          color: colors.structure.steel,
        },
        opacity: constants.opacity.full,
      }}
    >
      <CirclePlusSvg height={constants.icons.regular} />
    </Button>
  </div>
)

ModelMatrixResize.propTypes = {
  zoom: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixResize
