import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Button, { VARIANTS } from '../Button'

const TaskErrorTaskMenu = ({ projectId, taskId, setIsRetried, revalidate }) => {
  return (
    <div
      css={{
        display: 'flex',
        marginBottom: -spacing.small,
        paddingTop: spacing.comfy,
      }}
    >
      <Menu
        open="bottom-left"
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

                    setIsRetried(true)
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
  setIsRetried: PropTypes.func.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default TaskErrorTaskMenu
