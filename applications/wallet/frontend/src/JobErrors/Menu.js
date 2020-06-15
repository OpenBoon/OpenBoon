import PropTypes from 'prop-types'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonGear from '../Button/Gear'

const ACTIONS = [
  {
    name: 'Retry Task',
    action: 'retry',
  },
]

const JobErrorsMenu = ({ projectId, taskId, revalidate }) => {
  return (
    <Menu open="left" button={ButtonGear}>
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
                      `/api/v1/projects/${projectId}/tasks/${taskId}/${action}/`,
                      { method: 'PUT' },
                    )

                    revalidate()
                  }}
                  isDisabled={false}
                >
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

JobErrorsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  taskId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobErrorsMenu
