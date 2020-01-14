import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

import GearSvg from '../Icons/gear.svg'

const ACTIONS = [
  {
    name: 'Pause',
    action: 'pause',
  },
  {
    name: 'Resume',
    action: 'resume',
  },
  {
    name: 'Cancel',
    action: 'cancel',
  },
  {
    name: 'Restart',
    action: 'restart',
  },
  {
    name: 'Retry All Failures',
    action: 'retry_all_failures',
  },
]

const JobsMenu = ({ projectId, jobId, revalidate }) => {
  return (
    <Menu
      open="left"
      button={({ onBlur, onClick }) => (
        <Button
          className="gear"
          aria-label="Toggle Actions Menu"
          variant={VARIANTS.NEUTRAL}
          style={{
            color: colors.structure.coal,
            padding: spacing.moderate / 2,
            borderRadius: constants.borderRadius.round,
            ':hover': { backgroundColor: colors.structure.steel },
          }}
          onBlur={onBlur}
          onClick={onClick}
          isDisabled={false}>
          <GearSvg width={20} />
        </Button>
      )}>
      {({ onBlur, onClick }) => (
        <div>
          <ul>
            {ACTIONS.map(({ name, action }) => (
              <li key={action}>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={async () => {
                    onClick()

                    await fetcher(
                      `/api/v1/projects/${projectId}/jobs/${jobId}/${action}/`,
                      { method: 'PUT' },
                    )

                    revalidate()
                  }}
                  isDisabled={false}>
                  {name}
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Menu>
  )
}

JobsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobsMenu
