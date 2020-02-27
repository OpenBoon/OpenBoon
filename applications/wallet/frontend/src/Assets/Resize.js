import { colors, constants, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CirclePlusSvg from './circlePlus.svg'
import CircleMinusSvg from './circleMinus.svg'

const SIZE_CONTROL_HEIGHT = 42
const SIZE_CONTROL_WIDTH = 90

const AssetsResize = () => (
  <div
    css={{
      height: SIZE_CONTROL_HEIGHT,
      width: SIZE_CONTROL_WIDTH,
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
    }}>
    <Button variant={VARIANTS.ICON} css={{ opacity: constants.opacity.full }}>
      <CircleMinusSvg width={20} />
    </Button>
    <Button variant={VARIANTS.ICON} css={{ opacity: constants.opacity.full }}>
      <CirclePlusSvg width={20} />
    </Button>
  </div>
)

export default AssetsResize
