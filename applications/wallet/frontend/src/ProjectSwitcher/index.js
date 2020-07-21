import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

const ProjectSwitcher = ({ projectId }) => {
  const {
    pathname,
    query: { projectId: routerProjectId },
    asPath,
  } = useRouter()

  const { data: { results: projects = [] } = {} } = useSWR('/api/v1/projects/')

  if (!routerProjectId) return null

  if (!projectId || !projects.length === 0) return null

  const selectedProject = projects.find(({ id }) => id === projectId)

  if (!selectedProject) return null

  if (projects.length === 1) {
    return (
      <div
        css={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: `${spacing.small}px ${spacing.base}px`,
          color: colors.key.one,
        }}
      >
        {selectedProject.name}
      </div>
    )
  }

  return (
    <Menu
      open="right"
      button={({ onBlur, onClick, isMenuOpen }) => (
        <Button
          variant={VARIANTS.MENU}
          onBlur={onBlur}
          onClick={onClick}
          isDisabled={false}
        >
          <div
            css={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
            }}
          >
            {selectedProject.name}
            <ChevronSvg
              height={constants.iconSize}
              css={{
                marginLeft: spacing.base,
                transform: `${isMenuOpen ? 'rotate(-180deg)' : ''}`,
              }}
            />
          </div>
        </Button>
      )}
    >
      {({ onBlur, onClick }) => (
        <ul>
          {projects.map(({ id, name }) => (
            <li key={id}>
              <Link
                href={
                  id === projectId
                    ? pathname
                    : pathname.split('/').slice(0, 3).join('/')
                }
                as={
                  id === projectId
                    ? asPath
                    : pathname
                        .replace('[projectId]', id)
                        .split('/')
                        .slice(0, 3)
                        .join('/')
                }
                passHref
              >
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={onClick}
                  isDisabled={false}
                >
                  {name}
                </Button>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Menu>
  )
}

ProjectSwitcher.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default ProjectSwitcher
