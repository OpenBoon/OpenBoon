import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import MinimapSvg from '../Icons/minimap.svg'
import CirclePlusSvg from '../Icons/circlePlus.svg'
import CircleMinusSvg from '../Icons/circleMinus.svg'

import ModelMatrixMinimap from './Minimap'

const ModelMatrixResize = ({ matrix, settings, dispatch }) => {
  return (
    <div
      css={{
        position: 'absolute',
        bottom: spacing.normal,
        right: spacing.normal,
        zIndex: zIndex.layout.interactive,
      }}
    >
      <ModelMatrixMinimap
        matrix={matrix}
        settings={settings}
        isInteractive
        isOutOfDate={matrix.unappliedChanges}
      />

      <div
        css={{
          display: 'flex',
          border: constants.borders.regular.iron,
          borderRadius: constants.borderRadius.small,
          backgroundColor: colors.structure.lead,
          boxShadow: constants.boxShadows.default,
          opacity: constants.opacity.eighth,
          paddingTop: spacing.small,
          paddingBottom: spacing.small,
          paddingLeft: spacing.base,
          paddingRight: spacing.base,
        }}
      >
        <Button
          aria-label="Mini map"
          onClick={() => {
            dispatch({ isMinimapOpen: !settings.isMinimapOpen })
          }}
          variant={VARIANTS.NEUTRAL}
          css={{
            color: settings.isMinimapOpen
              ? colors.key.one
              : colors.structure.white,
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
          <MinimapSvg height={constants.icons.regular} />
        </Button>

        <div
          css={{
            backgroundColor: colors.structure.steel,
            width: spacing.hairline,
            margin: spacing.base,
          }}
        />

        <Button
          aria-label="Zoom Out"
          onClick={() => {
            if (settings.zoom === 1) return
            dispatch({ zoom: settings.zoom - 1 })
          }}
          isDisabled={settings.zoom === 1}
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
            dispatch({ zoom: settings.zoom + 1 })
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
    </div>
  )
}

ModelMatrixResize.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
    unappliedChanges: PropTypes.bool.isRequired,
  }).isRequired,
  settings: PropTypes.shape({
    zoom: PropTypes.number.isRequired,
    isMinimapOpen: PropTypes.bool.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixResize
