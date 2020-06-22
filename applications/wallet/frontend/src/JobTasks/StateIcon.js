import PropTypes from 'prop-types'

import { colors } from '../Styles'

import CrossSmallSvg from '../Icons/crossSmall.svg'
import ClockSvg from '../Icons/clock.svg'
import SquareSquareSquareSvg from '../Icons/squareSquareSquare.svg'
import PausedSvg from '../Icons/paused.svg'
import CheckmarkSvg from '../Icons/checkmark.svg'
import GeneratingSvg from '../Icons/generating.svg'

const WIDTH = 20

const JobTasksStateIcon = ({ state }) => {
  switch (state) {
    case 'Waiting':
    case 'Depend':
      return <ClockSvg color={colors.structure.white} width={WIDTH} />

    case 'Running':
      return <GeneratingSvg color={colors.signal.canary.base} width={WIDTH} />

    case 'Success':
      return <CheckmarkSvg color={colors.signal.grass.base} width={WIDTH} />

    case 'Skipped':
      return <PausedSvg color={colors.structure.steel} width={WIDTH} />

    case 'Queued':
      return (
        <SquareSquareSquareSvg color={colors.signal.sky.base} width={WIDTH} />
      )

    case 'Failure':
    default:
      return <CrossSmallSvg color={colors.signal.warning.base} width={WIDTH} />
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
