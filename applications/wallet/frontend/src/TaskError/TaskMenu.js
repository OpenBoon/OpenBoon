import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Button, { VARIANTS } from '../Button'

const TaskErrorTaskMenu = ({ projectId, taskId, revalidate }) => {
  return (
    <div
      css={{
        display: 'flex',
        marginBottom: -spacing.small,
        paddingTop: spacing.comfy,
      }}
    >
      <Menu
        open="left"
        button={({ onBlur, onClick }) => (
          <MenuButton onBlur={onBlur} onClick={onClick} legend="Modify Task" />
        )}
      >
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={async () => {
                    onClick()

                    await fetcher(
                      `/api/v1/projects/${projectId}/tasks/${taskId}/retry/`,
                      { method: 'PUT' },
                    )

                    revalidate()
                  }}
                >
                  Retry Task
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
    </div>
  )
}

TaskErrorTaskMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  taskId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default TaskErrorTaskMenu
