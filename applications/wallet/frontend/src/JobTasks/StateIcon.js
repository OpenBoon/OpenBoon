import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CrossSmallSvg from '../Icons/crossSmall.svg'
import ClockSvg from '../Icons/clock.svg'
import SquareSquareSquareSvg from '../Icons/squareSquareSquare.svg'
import PausedSvg from '../Icons/paused.svg'
import CheckmarkSvg from '../Icons/checkmark.svg'
import GeneratingSvg from '../Icons/generating.svg'

const JobTasksStateIcon = ({ state }) => {
  switch (state) {
    case 'Waiting':
    case 'Depend':
      return (
        <ClockSvg
          color={colors.structure.white}
          height={constants.icons.regular}
        />
      )

    case 'Running':
      return (
        <GeneratingSvg
          color={colors.signal.canary.base}
          height={constants.icons.regular}
        />
      )

    case 'Success':
      return (
        <CheckmarkSvg
          color={colors.signal.grass.base}
          height={constants.icons.regular}
        />
      )

    case 'Skipped':
      return (
        <PausedSvg
          color={colors.structure.steel}
          height={constants.icons.regular}
        />
      )

    case 'Queued':
      return (
        <SquareSquareSquareSvg
          color={colors.signal.sky.base}
          height={constants.icons.regular}
        />
      )

    case 'Failure':
    default:
      return (
        <CrossSmallSvg
          color={colors.signal.warning.base}
          height={constants.icons.regular}
        />
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
