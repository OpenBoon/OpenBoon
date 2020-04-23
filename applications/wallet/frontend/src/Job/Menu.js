import PropTypes from 'prop-types'
import Router from 'next/router'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Button, { VARIANTS } from '../Button'

import { ACTIONS } from './helpers'

const JobMenu = ({ projectId, jobId, status, revalidate }) => {
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
        open="left"
        button={({ onBlur, onClick }) => (
          <MenuButton onBlur={onBlur} onClick={onClick} legend="Modify Job" />
        )}
      >
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              {ACTIONS[status].map(({ name, action }) => (
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
                          pathname: '/[projectId]/jobs/[jobId]',
                          query: {
                            projectId,
                            jobId,
                            refreshParam: Math.random(),
                          },
                        },
                        `/${projectId}/jobs/${jobId}`,
                      )
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
    </div>
  )
}

JobMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  status: PropTypes.oneOf(Object.keys(ACTIONS)).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobMenu
