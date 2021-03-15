import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Button, { VARIANTS } from '../Button'

import { ACTIONS } from './helpers'

const JobMenu = ({ status, revalidate }) => {
  const {
    pathname,
    asPath,
    query: { projectId, jobId },
  } = useRouter()

  if (!ACTIONS[status].length) return null

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
          <MenuButton onBlur={onBlur} onClick={onClick} legend="Modify Job" />
        )}
      >
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              {ACTIONS[status].map(({ name, action, confirmation }) => (
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

                      Router.push(
                        {
                          pathname,
                          query: {
                            projectId,
                            jobId,
                            refreshParam: Math.random(),
                            action: confirmation,
                          },
                        },
                        asPath,
                      )
                    }}
                  >
                    {name}
                  </Button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </Menu>
    </div>
  )
}

JobMenu.propTypes = {
  status: PropTypes.oneOf(Object.keys(ACTIONS)).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobMenu
