import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Button, { VARIANTS } from '../Button'

const TaskMenu = ({ revalidate }) => {
  const {
    pathname,
    asPath,
    query: { projectId, jobId, taskId },
  } = useRouter()

  return (
    <div
      css={{
        display: 'flex',
        marginBottom: -spacing.small,
        paddingRight: spacing.giant,
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
                          projectId,
                          jobId,
                          taskId,
                          refreshParam: Math.random(),
                          action: 'Retrying task.',
                        },
                      },
                      asPath,
                    )
                  }}
                >
                  Retry
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
    </div>
  )
}

TaskMenu.propTypes = {
  revalidate: PropTypes.func.isRequired,
}

export default TaskMenu
