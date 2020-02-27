import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const JobErrorTaskMenu = ({ projectId, taskId, revalidate }) => {
  return (
    <div
      css={{
        display: 'flex',
        marginBottom: -spacing.small,
        paddingTop: spacing.comfy,
      }}>
      <Menu
        open="left"
        button={({ onBlur, onClick }) => (
          <Button
            aria-label="Toggle Task Actions Menu"
            variant={VARIANTS.DROPDOWN}
            onBlur={onBlur}
            onClick={onClick}
            isDisabled={false}>
            Modify Task
            <ChevronSvg width={20} />
          </Button>
        )}>
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
                  isDisabled={false}>
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

JobErrorTaskMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  taskId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobErrorTaskMenu
