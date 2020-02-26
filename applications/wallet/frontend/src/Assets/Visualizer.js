import { spacing, colors, constants } from '../Styles'

import CirclePlusSvg from './circlePlus.svg'
import CircleMinusSvg from './circleMinus.svg'

import { WIDTH as METADATA_WIDTH } from './Metadata'

const SIZE_CONTROL_HEIGHT = 42
const SIZE_CONTROL_WIDTH = 90

const AssetsVisualizer = () => {
  return (
    <div
      css={{
        float: 'left',
        backgroundColor: 'white',
        margin: -spacing.spacious,
        paddingTop: spacing.spacious,
        height: `calc(100% + ${spacing.spacious * 2}px)`,
        width: `calc(100% - ${METADATA_WIDTH}px + ${spacing.spacious * 2}px)`,
      }}>
      <div
        css={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: SIZE_CONTROL_HEIGHT,
          width: SIZE_CONTROL_WIDTH,
          border: constants.borders.tab,
          borderRadius: constants.borderRadius.small,
          backgroundColor: colors.structure.lead,
          boxShadow: '0 2px 4px 0 rgba(0,0,0,0.5)',
        }}>
        <CircleMinusSvg width={30} color={colors.structure.smoke} />
        <CirclePlusSvg width={20} color={colors.key.one} />
      </div>
    </div>
  )
}

export default AssetsVisualizer
