import { spacing, colors, constants, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CirclePlusSvg from './circlePlus.svg'
import CircleMinusSvg from './circleMinus.svg'

import { WIDTH as METADATA_WIDTH } from './Metadata'

const SIZE_CONTROL_HEIGHT = 42
const SIZE_CONTROL_WIDTH = 90

const AssetsVisualizer = () => {
  return (
    <div
      css={{
        padding: spacing.spacious,
        overflow: 'auto',
        height: `calc(100% + ${spacing.spacious}px)`,
        width: `calc(100% - ${METADATA_WIDTH}px)`,
      }}>
      <div
        css={{
          height: '100%',
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'flex-end',
          padding: spacing.normal,
        }}>
        <div
          css={{
            position: 'fixed',
            bottom: spacing.normal,
            right: 400 + spacing.normal,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: SIZE_CONTROL_HEIGHT,
            width: SIZE_CONTROL_WIDTH,
            border: constants.borders.tab,
            borderRadius: constants.borderRadius.small,
            backgroundColor: colors.structure.lead,
            boxShadow: constants.boxShadows.default,
            zIndex: zIndex.layout.overlay,
            opacity: 0.8,
          }}>
          <Button
            variant={VARIANTS.ICON}
            css={{ opacity: constants.opacity.full }}>
            <CircleMinusSvg width={20} />
          </Button>
          <Button
            variant={VARIANTS.ICON}
            css={{ opacity: constants.opacity.full }}>
            <CirclePlusSvg width={20} />
          </Button>
        </div>
      </div>
    </div>
  )
}

export default AssetsVisualizer
