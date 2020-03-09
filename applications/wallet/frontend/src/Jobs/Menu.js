import PropTypes from 'prop-types'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonGear from '../Button/Gear'

const ACTIONS = {
  InProgress: [
    {
      name: 'Pause',
      action: 'pause',
    },
    {
      name: 'Cancel',
      action: 'cancel',
    },
  ],
  Cancelled: [
    {
      name: 'Restart',
      action: 'restart',
    },
  ],
  Success: [],
  Archived: [],
  Failure: [
    {
      name: 'Retry All Failures',
      action: 'retry_all_failures',
    },
  ],
  Paused: [
    {
      name: 'Resume',
      action: 'resume',
    },
  ],
}

const JobsMenu = ({ projectId, jobId, jobStatus, revalidate }) => {
  if (!ACTIONS[jobStatus].length) return null

  return (
    <Menu open="left" button={ButtonGear}>
      {({ onBlur, onClick }) => (
        <div>
          <ul>
            {ACTIONS[jobStatus].map(({ name, action }) => (
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
  jobStatus: PropTypes.oneOf(Object.keys(ACTIONS)).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobsMenu
