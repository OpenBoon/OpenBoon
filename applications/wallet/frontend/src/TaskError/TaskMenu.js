import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Button, { VARIANTS } from '../Button'

const TaskErrorTaskMenu = ({ taskId, revalidate }) => {
  const {
    pathname,
    asPath,
    query: { projectId },
  } = useRouter()

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

                    revalidate()

                    Router.push(
                      {
                        pathname,
                        query: {
                          action: 'Retrying task.',
                        },
                      },
                      asPath,
                    )
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
  taskId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default TaskErrorTaskMenu
