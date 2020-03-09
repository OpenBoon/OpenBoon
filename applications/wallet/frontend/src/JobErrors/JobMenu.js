import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Button, { VARIANTS } from '../Button'

const ACTIONS = [
  {
    name: 'Pause',
    action: 'pause',
  },
  {
    name: 'Resume',
    action: 'resume',
  },
  {
    name: 'Cancel',
    action: 'cancel',
  },
  {
    name: 'Restart',
    action: 'restart',
  },
  {
    name: 'Retry All Failures',
    action: 'retry_all_failures',
  },
]

const JobErrorsJobMenu = ({ projectId, jobId, revalidate }) => {
  return (
    <div css={{ display: 'flex', marginBottom: -spacing.small }}>
      <Menu
        open="left"
        button={({ onBlur, onClick }) => (
          <MenuButton onBlur={onBlur} onClick={onClick} legend="Modify Job" />
        )}>
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
    </div>
  )
}

JobErrorsJobMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobErrorsJobMenu
