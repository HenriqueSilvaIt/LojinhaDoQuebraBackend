package com.devsuperior.dscommerce.services;

import com.devsuperior.dscommerce.dto.*;
import com.devsuperior.dscommerce.entities.*;
import com.devsuperior.dscommerce.repositories.RoleRepository;
import com.devsuperior.dscommerce.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsuperior.dscommerce.repositories.UserRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

	@Autowired
	private UserRepository repository;

	@Autowired
	private RoleRepository roleRepository; // Inject RoleRepository


	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		User user = repository.findByEmail(username);
		if (user == null) {
			throw new UsernameNotFoundException("Email not found");
		}
		return user;
	}
	
	protected User authenticated() {
		try {
			String username = SecurityContextHolder.getContext().getAuthentication().getName();
			return repository.findByEmail(username);
		}
		catch (Exception e) {
			throw new UsernameNotFoundException("Invalid user");
		}
	}

	@Transactional(readOnly = true)
	public UserDTO findById(Long id) {
		User user = repository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException("Recurso não encontrado"));
		return new UserDTO(user);
	}

	@Transactional(readOnly = true)
	public UserDTO getMe() {
		User entity = authenticated();
		return new UserDTO(entity);
	}

	@Transactional
	public UserDTO insertUser(UserDTO dto) {
		User entity = new User();
		copyDtoToEntity(dto, entity);
		entity = repository.save(entity);
		return  new UserDTO(entity);
	}

	@Transactional
	public RoleDTO insertRole(RoleDTO dto) {
		Role entity = new Role();
		entity.setAuthority(dto.getAuthority());
		entity = roleRepository.save(entity);
		return new RoleDTO(entity);
	}

	@Transactional
	public UserDTO update(Long id, UserDTO dto) {
		try {
			User entity = repository.getReferenceById(id);
			copyDtoToEntity(dto, entity);
			entity = repository.save(entity);
			return new UserDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Recurso não encontrado");
		}
	}


	@Transactional
	public UserDTO addRoleToUser(Long userId, Long roleId) {
		User user = repository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Role role = roleRepository.findById(roleId)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found"));

		user.getRoles().add(role);
		user = repository.save(user);
		return new UserDTO(user);
	}

	private void copyDtoToEntity(UserDTO dto, User entity) {
		entity.setName(dto.getName());
		entity.setEmail(dto.getEmail());
		entity.setPhone(dto.getPhone());
		entity.setPassword(dto.getPassword());
		entity.setBirthDate(dto.getBirthDate());

	/*	entity.getRoles().clear();
		for (UserDTO : entity.getId() {
			Role = new Role();
			role.setId(role.getId());
			entity.getRoles().add(role);


			   entity.getCategories().clear();
        for (CategoryDTO catDto : dto.getCategories()) {
        	Category cat = new Category();
        	cat.setId(catDto.getId());
        	entity.getCategories().add(cat);

   	entity.getRoles().clear();
		for (GrantedAuthority role : entity.getAuthorities() ) {
			Role roleDTO = new Role();
			roleDTO.setId(roleDTO.getId());
			entity.getRoles().add(roleDTO);
		}

        		for (GrantedAuthority role : entity.getAuthorities()) {
			roles.add(role.getAuthority());
				for (GrantedAuthority role : entity.getAuthorities()) {
			roles.add(role.getAuthority());
		}
		}

			entity.getRoles().clear();
		for (GrantedAuthority role : entity.getAuthorities() ) {
			Role roleDTO = new Role();
			roleDTO.setAuthority(role.getAuthority());
			entity.getRoles().add(roleDTO);
		}

		}*/

		entity.getRoles().clear();
		for (GrantedAuthority role : entity.getAuthorities()) {
			Role roleDTO = new Role();
			roleDTO.setAuthority(role.getAuthority());
			entity.getRoles().add(roleDTO);
		}


	}
}
