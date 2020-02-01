import PropTypes from 'prop-types'

import { spacing, colors } from '../Styles'

import { fetcher } from '../Fetch/helpers'

import Menu, { WIDTH } from '../Menu'
import Button, { VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

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
          <Button
            aria-label="Toggle Job Actions Menu"
            variant={VARIANTS.SECONDARY}
            css={{
              width: WIDTH,
              paddingTop: spacing.base,
              paddingBottom: spacing.base,
              paddingLeft: spacing.normal,
              paddingRight: spacing.normal,
              flexDirection: 'row',
              justifyContent: 'space-between',
              marginBottom: spacing.small,
              color: colors.structure.black,
            }}
            onBlur={onBlur}
            onClick={onClick}
            isDisabled={false}>
            Modify Job
            <ChevronSvg width={20} />
          </Button>
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
