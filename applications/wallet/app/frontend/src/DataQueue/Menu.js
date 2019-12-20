import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

import GearSvg from '../Icons/gear.svg'

const DataQueueMenu = ({ projectId, jobId }) => {
  return (
    <Menu
      button={({ onBlur, onClick }) => (
        <Button
          aria-label="Toggle Actions Menu"
          variant={VARIANTS.NEUTRAL}
          style={{
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
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={async () => {
                  await fetcher(
                    `/api/v1/projects/${projectId}/jobs/${jobId}/pause/`,
                    { method: 'PUT' },
                  )
                }}
                isDisabled={false}>
                Pause
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={onClick}
                isDisabled={false}>
                Resume
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={onClick}
                isDisabled={false}>
                Cancel
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={onClick}
                isDisabled={false}>
                Restart
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={onClick}
                isDisabled={false}>
                Retry All Failures
              </Button>
            </li>
          </ul>
        </div>
      )}
    </Menu>
  )
}

DataQueueMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
}

export default DataQueueMenu
