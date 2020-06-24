import PropTypes from 'prop-types'

import { colors } from '../Styles'

import CrossSmallSvg from '../Icons/crossSmall.svg'
import ClockSvg from '../Icons/clock.svg'
import SquareSquareSquareSvg from '../Icons/squareSquareSquare.svg'
import PausedSvg from '../Icons/paused.svg'
import CheckmarkSvg from '../Icons/checkmark.svg'
import GeneratingSvg from '../Icons/generating.svg'

const ICON_SIZE = 20

const JobTasksStateIcon = ({ state }) => {
  switch (state) {
    case 'Waiting':
    case 'Depend':
      return <ClockSvg color={colors.structure.white} width={ICON_SIZE} />

    case 'Running':
      return (
        <GeneratingSvg color={colors.signal.canary.base} width={ICON_SIZE} />
      )

    case 'Success':
      return <CheckmarkSvg color={colors.signal.grass.base} width={ICON_SIZE} />

    case 'Skipped':
      return <PausedSvg color={colors.structure.steel} width={ICON_SIZE} />

    case 'Queued':
      return (
        <SquareSquareSquareSvg
          color={colors.signal.sky.base}
          width={ICON_SIZE}
        />
      )

    case 'Failure':
    default:
      return (
        <CrossSmallSvg color={colors.signal.warning.base} width={ICON_SIZE} />
      )
  }
}

JobTasksStateIcon.propTypes = {
  state: PropTypes.oneOf([
    'Waiting',
    'Depend',
    'Running',
    'Success',
    'Failure',
    'Skipped',
    'Queued',
  ]).isRequired,
}

export default JobTasksStateIcon
