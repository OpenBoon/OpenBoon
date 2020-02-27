import { colors, constants, spacing, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CirclePlusSvg from './circlePlus.svg'
import CircleMinusSvg from './circleMinus.svg'

const AssetsResize = () => (
  <div
    css={{
      position: 'absolute',
      bottom: 16,
      right: 16,
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      border: constants.borders.tab,
      borderRadius: constants.borderRadius.small,
      backgroundColor: colors.structure.lead,
      boxShadow: constants.boxShadows.default,
      zIndex: zIndex.layout.overlay,
      opacity: constants.opacity.eighth,
      paddingTop: spacing.small,
      paddingBottom: spacing.small,
      paddingLeft: spacing.base,
      paddingRight: spacing.base,
    }}>
    <Button
      variant={VARIANTS.ICON}
      css={{ opacity: constants.opacity.full, padding: spacing.base }}>
      <CircleMinusSvg width={20} />
    </Button>
    <Button
      variant={VARIANTS.ICON}
      css={{ opacity: constants.opacity.full, padding: spacing.base }}>
      <CirclePlusSvg width={20} />
    </Button>
  </div>
)

export default AssetsResize
